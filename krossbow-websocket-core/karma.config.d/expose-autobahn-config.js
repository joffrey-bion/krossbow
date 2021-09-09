const fs = require('fs');
const webpack = require('webpack');

// This is to transfer the autobahn config from gradle to the browser environment
config.webpack.plugins.push(
    new webpack.DefinePlugin({
        "autobahn" : fs.readFileSync('./autobahn-server.json', 'utf8')
    })
)
