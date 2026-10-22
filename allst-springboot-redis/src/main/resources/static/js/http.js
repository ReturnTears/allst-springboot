async function api(url, options) {
  const headers = Object.assign({ "Content-Type": "application/json" }, (options && options.headers) || {});
  const token = localStorage.getItem("redis-demo-token");
  if (token) {
    headers["X-Token"] = token;
  }
  const response = await fetch(url, Object.assign({}, options, { headers: headers }));
  const text = await response.text();
  let body = text;
  try {
    body = JSON.parse(text);
  } catch (ignored) {
    body = { message: text };
  }
  if (!response.ok) {
    throw new Error((body && body.message) || response.statusText);
  }
  return body.data;
}
