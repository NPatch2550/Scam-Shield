const { createLogger, format, transports } = require('winston');

const logger = createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: format.combine(
    format.timestamp({ format: 'HH:mm:ss' }),
    format.colorize(),
    format.printf(({ timestamp, level, message, ...rest }) => {
      const extra = Object.keys(rest).length ? ' ' + JSON.stringify(rest) : '';
      return `[${timestamp}] ${level}: ${message}${extra}`;
    })
  ),
  transports: [new transports.Console()],
});

module.exports = logger;
