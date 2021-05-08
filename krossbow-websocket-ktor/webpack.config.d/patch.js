// workaround for kotlin 1.5 'crypto' polyfill error
// https://youtrack.jetbrains.com/issue/KT-46082
config.resolve.alias = {
  "crypto": false,
}
