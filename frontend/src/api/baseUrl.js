export function resolveApiBaseUrl(isProduction, configuredBaseUrl) {
  return isProduction ? '/api/v1' : (configuredBaseUrl || '/api/v1');
}
