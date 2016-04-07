var path = require('path');
var webpack = require('webpack');

module.exports = {
  devtool: 'sourcemap',
  entry: [
    'webpack-dev-server/client?http://localhost:3000',
    'webpack/hot/only-dev-server',
    './src/js/app.jsx'
  ],
  output: {
    path: path.join(__dirname, 'resources/public/js/gen/'),
    filename: 'bundle.js',
    publicPath: 'http://localhost:3000/resources/public/js/gen/'
  },
  plugins: [
    new webpack.HotModuleReplacementPlugin()
  ],
  module: {
    loaders: [{
      test: /\.jsx$|\.js$/,
      exclude: /node_modules/,
      loaders: ['react-hot', 'babel'],
      include: path.join(__dirname, 'src/js')
    }]
  }
};
