/**
 * Centralized API error response handler with safe diagnostic logging.
 * Never leaks database connection strings, passwords, or JWT secrets.
 */

function sanitizeError(msg) {
  if (!msg) return '';
  return String(msg)
    // Redact database credentials: postgres://user:password@host
    .replace(/:\/\/[^:]+:([^@]+)@/g, '://***:***@')
    // Redact potential JWT tokens
    .replace(/eyJ[a-zA-Z0-9_-]+\.[a-zA-Z0-9_-]+\.[a-zA-Z0-9_-]+/g, '[REDACTED_JWT]');
}

function extractErrorMessage(err) {
  if (!err) return 'An unexpected server error occurred.';

  // If AggregateError, extract nested network/database errors
  if (err.errors && Array.isArray(err.errors) && err.errors.length > 0) {
    return err.errors.map(e => e.message || e.code || 'Connection refused').join('; ');
  }

  if (err.message && err.message.trim().length > 0) {
    return err.message.trim();
  }

  if (err.code) {
    return `Database error [${err.code}]`;
  }

  return err.toString() || 'An internal server error occurred.';
}

function errorHandler(err, req, res, next) {
  const rawMsg = extractErrorMessage(err);
  const safeMsg = sanitizeError(rawMsg);

  console.error(`❌ [API Error] ${req.method} ${req.path}:`, {
    status: err.statusCode || 500,
    message: safeMsg,
    code: err.code || undefined,
  });

  const statusCode = err.statusCode || 500;

  res.status(statusCode).json({
    success: false,
    message: safeMsg,
    code: err.code || undefined,
    endpoint: req.path,
    ...(process.env.NODE_ENV !== 'production' ? { stack: err.stack } : {}),
  });
}

module.exports = errorHandler;
