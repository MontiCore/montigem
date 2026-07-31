module.exports = {
  "/umlp/api": {
    target: process.env.BACKEND_PROXY_ADDRESS || "http://localhost:8081/",
    secure: false,
    ws: true,
    logLevel: "silent",
    onError: function(err, req, res) {
      console.error("Proxy Error: Failed to connect to the backend API. Check if the server is started.");
    }
  },
  "/scans": {
    target: "http://localhost:5000",
    secure: false,
    logLevel: "silent",
    onError: function(err, req, res) {
      console.error("Proxy Error: Failed to connect to the scan server. Check if the server is started.");
    }
  },
  "/pick": {
    target: "http://localhost:5000",
    secure: false,
    logLevel: "silent",
    onError: function(err, req, res) {
      console.error("Proxy Error: Failed to connect to the scan server. Check if the server is started.");
    }
  },
  "/locate": {
    target: "http://localhost:5000",
    secure: false,
    logLevel: "silent",
    onError: function(err, req, res) {
      console.error("Proxy Error: Failed to connect to the scan server. Check if the server is started.");
    }
  }

}
