# To run these you can use https://github.com/DarthSim/overmind
# NB! Currently the server_js process depends on ui_js running
#     And server can't be started until deploy contracts has completed
#     So the current way would be to run these one by one starting from testnet
testnet: bb testnet
deploy_contracts: sleep 5 && npx truffle migrate --network development --reset && sleep infinity # Wait a bit for testnet to start
css: bb compile-css && bb watch-css
ui_js: bb watch-ui
server_js: bb watch-server
server: bb run-server
