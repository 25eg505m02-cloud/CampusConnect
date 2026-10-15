export function resolveApiBaseUrl(isProduction, configuredBaseUrl, productionBaseUrl) {
  return isProduction
    ? productionBaseUrl
    : (configuredBaseUrl || '/api/v1');
}
