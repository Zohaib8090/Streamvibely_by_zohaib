
const createExpoWebpackConfigAsync = require('@expo/webpack-config');

module.exports = async function (env, argv) {
  const config = await createExpoWebpackConfigAsync(env, argv);

  config.module.rules.push({
    test: /\.ttf$/,
    loader: 'url-loader', 
    include: require.resolve('@expo/vector-icons').split('build')[0],
  });

  return config;
};
