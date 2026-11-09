import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        String hostname = System.getenv().getOrDefault("HOSTNAME", "local-machine");

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Main page: serve a fresh game with a random word, stamped with this server's hostname
        server.createContext("/", exchange -> {
            String html = PAGE
                .replace("__HOSTNAME__", hostname)
                .replace("__ANSWER__", WordList.random());
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });

        // Health endpoint — Kubernetes will hit this to check the pod is alive
        server.createContext("/health", exchange -> {
            byte[] bytes = "OK".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });

        server.setExecutor(null);
        System.out.println("Wordle Web listening on :" + port + " (host: " + hostname + ")");
        server.start();
    }

    // The HTML page sent to the browser. Game logic lives in JavaScript so the
    // Java server stays stateless — any pod can handle any request.
    private static final String PAGE = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <title>Wordle Web</title>
              <style>
                body { font-family: -apple-system, system-ui, sans-serif; max-width: 480px;
                       margin: 30px auto; text-align: center; background: #fafafa; }
                h1 { margin-bottom: 4px; }
                .pod { color: #666; font-size: 13px; margin-bottom: 24px; }
                .pod code { background: #eee; padding: 2px 6px; border-radius: 3px; }
                .row { margin: 4px 0; }
                .cell { display: inline-block; width: 44px; height: 44px; line-height: 44px;
                        border: 2px solid #d3d6da; margin: 2px; font-weight: bold;
                        font-size: 22px; background: white; }
                .correct { background: #6aaa64; color: white; border-color: #6aaa64; }
                .present { background: #c9b458; color: white; border-color: #c9b458; }
                .absent  { background: #787c7e; color: white; border-color: #787c7e; }
                input { font-size: 18px; padding: 8px; width: 140px; text-transform: uppercase;
                        border: 2px solid #d3d6da; border-radius: 4px; }
                button { font-size: 18px; padding: 8px 16px; margin-left: 4px; border: none;
                         background: #6aaa64; color: white; border-radius: 4px; cursor: pointer; }
                #status { margin-top: 18px; font-weight: bold; min-height: 24px; }
              </style>
            </head>
            <body>
              <h1>Wordle Web</h1>
              <div class="pod">Served by: <code>__HOSTNAME__</code></div>
              <div id="board"></div>
              <div>
                <input id="guess" maxlength="5" autocomplete="off" autofocus />
                <button onclick="submit()">Guess</button>
              </div>
              <div id="status"></div>
              <script>
                const answer = "__ANSWER__";
                let attempts = 0;
                const MAX = 6;
                function submit() {
                  const input = document.getElementById('guess');
                  const guess = input.value.toUpperCase();
                  if (guess.length !== 5) { setStatus('Must be 5 letters'); return; }
                  const counts = {};
                  for (const ch of answer) counts[ch] = (counts[ch] || 0) + 1;
                  const states = new Array(5).fill('absent');
                  for (let i = 0; i < 5; i++) {
                    if (guess[i] === answer[i]) { states[i] = 'correct'; counts[guess[i]]--; }
                  }
                  for (let i = 0; i < 5; i++) {
                    if (states[i] === 'absent' && counts[guess[i]] > 0) {
                      states[i] = 'present'; counts[guess[i]]--;
                    }
                  }
                  const row = document.createElement('div');
                  row.className = 'row';
                  for (let i = 0; i < 5; i++) {
                    const cell = document.createElement('div');
                    cell.className = 'cell ' + states[i];
                    cell.textContent = guess[i];
                    row.appendChild(cell);
                  }
                  document.getElementById('board').appendChild(row);
                  input.value = '';
                  attempts++;
                  if (guess === answer) {
                    setStatus('You won in ' + attempts + '! Refresh for a new word.');
                    input.disabled = true;
                  } else if (attempts >= MAX) {
                    setStatus('Out of attempts. The word was: ' + answer);
                    input.disabled = true;
                  }
                }
                function setStatus(m) { document.getElementById('status').textContent = m; }
                document.getElementById('guess').addEventListener('keydown', e => {
                  if (e.key === 'Enter') submit();
                });
              </script>
            </body>
            </html>
            """;
}
