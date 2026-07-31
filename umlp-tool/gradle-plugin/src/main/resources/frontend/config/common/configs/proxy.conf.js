module.exports = {
  "/umlp/api": {
    target: process.env.BACKEND_PROXY_ADDRESS || "http://localhost:8081/",
    secure: false,
    ws: true,
    logLevel: "silent",
    onError: function(err, req, res) {
      console.error("Proxy Error: Failed to connect to the backend API. Check if the server is started.");
    }
  }

}
