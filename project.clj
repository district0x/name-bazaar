(defproject name-bazaar "1.0.0"
  :description "A peer-to-peer marketplace for the exchange of names registered via the Ethereum Name Service."
  :url "https://github.com/district0x/name-bazaar"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[cljs-http "0.1.46"]
                 [cljs-web3 "0.19.0-0-11"]
                 [cljsjs/filesaverjs "1.3.3-0"]
                 [cljsjs/prop-types "15.6.0-0"]
                 [cljsjs/react "16.4.1-0"]
                 [cljsjs/react-datepicker "1.5.0-0"]
                 [cljsjs/react-dom "16.4.1-0"]
                 [cljsjs/react-infinite "0.13.0-0"]
                 [cljsjs/react-meta-tags "0.3.0-1"]
                 [com.rpl/specter "1.1.1"]
                 [compojure "1.6.2"]
                 [day8.re-frame/async-flow-fx "0.0.8"]
                 [day8.re-frame/forward-events-fx "0.0.5"]
                 [honeysql "0.9.3"]
                 [medley "1.0.0"]
                 [org.clojure/clojurescript "1.10.439"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [print-foo-cljs "2.0.3"]
                 [re-frame "0.10.6"]
                 [re-promise "0.1.1"]
                 [ring/ring-defaults "0.3.2"]
                 ;; Can be removed when re-frame vbump includes reagent 8.0.1+
                 [reagent "0.8.1"]
                 [soda-ash "0.76.0"]

                 [district0x/bignumber "1.0.3"]
                 [district0x/district-encryption "1.0.1"]
                 [district0x/district-sendgrid "1.0.1"]
                 [district0x/district-server-config "1.0.1"]
                 [district0x/district-server-db "1.0.4"]
                 ;; TODO(not important): Update to latest version.
                 ;; Version 1.0.2 splits the package into two separate modules.
                 [district0x/district-server-endpoints "1.0.1"]
                 [district0x/district-server-logging "1.0.6"]
                 ;; Version 1.0.11, containing changes from the following PR
                 ;; https://github.com/district0x/district-server-smart-contracts/pull/9
                 ;; removes the :auto-mining? option in :smart-contracts, which is used
                 ;; in dev mode and tests. Later version removes deploy-smart-contract!
                 ;; function in favor of truffle migrations.
                 ;; TODO(not important): update to latest version
                 ;;
                 ;; Versions up to 1.0.8 do not trigger errors in BE, but starting from
                 ;; 1.0.9 function `get-offering` is being called with `nil` and there is
                 ;; an web3 error + db error. However, until 1.0.9 it seems that contract
                 ;; callbacks do not work at all. So there is a bug either way.
                 ;; TODO: update to version 1.0.9
                 [district0x/district-server-smart-contracts "1.0.1"]
                 ;; TODO: update to newer version. Updating to 1.1.0 breaks the tests
                 [district0x/district-server-web3 "1.0.1"]
                 [district0x/district-server-web3-watcher "1.0.3"]
                 [district0x/district-ui-logging "1.1.0"]
                 [district0x/district-ui-mobile "1.0.0"]
                 [district0x/error-handling "1.0.4"]

                 ;; d0xINFRA temporary here
                 [akiroz.re-frame/storage "0.1.2"]
                 [bidi "2.1.2"]
                 [mount "0.1.11"]
                 [camel-snake-kebab "0.4.0"]
                 [cljs-ajax "0.7.2"]
                 [cljsjs/bignumber "2.1.4-1"]
                 [cljsjs/react-flexbox-grid "1.0.0-0"]
                 [cljsjs/react-highlight "1.0.7-2"]
                 [cljsjs/react-truncate "2.0.3-0"]
                 [cljsjs/react-ultimate-pagination "0.8.0-0"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.cognitect/transit-cljs "0.8.243"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.taoensso/encore "2.92.0"]
                 [day8.re-frame/http-fx "0.1.4"]
                 [kibu/pushy "0.3.8"]
                 [madvas.re-frame/google-analytics-fx "0.1.0"]
                 [madvas/cemerick-url-patched "0.1.2-SNAPSHOT"] ;; Temporary until cemerick merges PR26
                 [madvas.re-frame/web3-fx "0.2.3"]]

  :exclusions [[com.taoensso/encore]
               [org.clojure/clojure]
               [org.clojure/clojurescript]]

  :plugins [[lein-auto "0.1.2"]
            [lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.18"]
            [lein-shell "0.5.0"]
            [lein-doo "0.1.8"]
            [lein-npm "0.6.2"]
            [lein-pdo "0.1.1"]]

  :npm {:dependencies [["@ensdomains/buffer" "0.0.10"]
                       ["@ensdomains/dnssec-oracle" "0.1.2"]
                       ["@ensdomains/ens" "0.4.5"]
                       ["@ensdomains/ethregistrar" "2.0.4"]
                       ["@ensdomains/resolver" "0.2.4"]
                       ["@sentry/node" "4.2.1"]
                       ;; https://github.com/district0x/district-server-db/blob/3839edd/project.clj#L12
                       ;; After deployment changes, (transitive) dependencies are not installed automatically
                       ;; TODO: remove, they should be installed automatically
                       [better-sqlite3 "5.4.0"]
                       [chalk "2.3.0"]
                       ;; https://github.com/district0x/district-server-smart-contracts/blob/682e649/project.clj#L17
                       ;; After deployment changes, (transitive) dependencies are not installed automatically
                       ;; TODO: remove, they should be installed automatically
                       [deasync "0.1.11"]
                       ;; https://github.com/district0x/district-encryption/blob/d8ff0f/project.clj#L9
                       ;; After deployment changes, (transitive) dependencies are not installed automatically
                       ;; TODO: remove, they should be installed automatically
                       [eccjs "0.3.1"]
                       [eth-ens-namehash "2.0.0"]
                       [semantic-ui "2.4.1"]
                       [source-map-support "0.4.0"]
                       [ws "2.3.1"]
                       [xhr2 "0.1.4"]]
        :devDependencies [["@testing-library/cypress" "7.0.3"]
                          ["@truffle/hdwallet-provider" "1.2.2"]
                          [ethlint "1.2.5"]
                          [cypress "6.4.0"]
                          [jsedn "0.4.1"]
                          [truffle "5.2.3"]]
        :package {:scripts {:ethlint "./node_modules/ethlint/bin/solium.js --dir resources/public/contracts/src/"
                            :ethlint-fix"./node_modules/ethlint/bin/solium.js --dir resources/public/contracts/src/ --fix"
                            :cypress-open " ./node_modules/cypress/bin/cypress open"
                            :cypress-run " ./node_modules/cypress/bin/cypress run"
                            }}}

  :min-lein-version "2.5.3"

  :source-paths ["src" "test"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "target"
                                    "dev-server"
                                    "server-tests"]

  :figwheel {:server-port 4544
             :css-dirs ["resources/public/css"]
             :repl-eval-timeout 30000 ;; 30 seconds
             :ring-handler user/route-handler}

  :auto {"compile-solidity" {:file-pattern #"\.(sol)$"
                             :paths ["resources/public/contracts/src"]}}

  :aliases {"compile-solidity" ["shell" "./compile-solidity.sh"]
            "clean-prod-server" ["shell" "rm" "-rf" "server"]
            "watch-css" ["shell" "./semantic.sh" "watch"]
            "build-css" ["shell" "./semantic.sh" "build-css"]
            "build-prod-server" ["do" ["clean-prod-server"] ["cljsbuild" "once" "server"]]
            "build-prod-ui" ["do" ["clean"] ["cljsbuild" "once" "min"]]
            "build-prod" ["pdo" ["build-prod-server"] ["build-prod-ui"] ["build-css"]]
            "run-slither" ["shell" "./run-slither.sh"]}

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [binaryage/devtools "0.9.10"]
                                  [cider/piggieback "0.4.0"]
                                  [figwheel-sidecar "0.5.18"]
                                  [day8.re-frame/re-frame-10x "0.3.6"]
                                  [day8.re-frame/tracing "0.5.1"]]
                   :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
                   :source-paths ["dev" "src"]
                   :resource-paths ["resources"]}}

  :cljsbuild {:builds [;; Development on client-side UI, which uses a testnet
                       {:id "dev-ui"
                        :source-paths ["src"]
                        :figwheel {:on-jsload "name-bazaar.ui.core/mount-root"}
                        :compiler {:main "name-bazaar.ui.core"
                                   :output-to "resources/public/js/compiled/app.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :asset-path "/js/compiled/out"
                                   :source-map-timestamp true
                                   :optimizations :none
                                   :preloads [print.foo.preloads.devtools
                                              day8.re-frame-10x.preload]
                                   :closure-defines {name-bazaar.ui.config.environment "dev"
                                                     "re_frame.trace.trace_enabled_QMARK_" true}
                                   :external-config {:devtools/config {:features-to-install :all}}}}

                       ;; Development on server-side with testnet
                       {:id "dev-server"
                        :source-paths ["src" "dev"]
                        :figwheel {:on-jsload "name-bazaar.server.dev/on-jsload"}
                        :compiler {:main "cljs.user"
                                   :output-to "dev-server/name-bazaar.js",
                                   :output-dir "dev-server",
                                   :target :nodejs,
                                   :optimizations :none,
                                   :closure-defines {goog.DEBUG true}
                                   :source-map true}}

                       ;; Production on server-side with mainnet
                       {:id "server"
                        :source-paths ["src"]
                        :compiler {:main "name-bazaar.server.core"
                                   :output-to "server/name-bazaar.js",
                                   :output-dir "server",
                                   :target :nodejs,
                                   :optimizations :simple,
                                   :source-map "server/name-bazaar.js.map"
                                   :closure-defines {goog.DEBUG false}
                                   :pretty-print false}}

                       ;; Production on client-side with mainnet
                       {:id "min"
                        :source-paths ["src"]
                        :compiler {:main "name-bazaar.ui.core"
                                   :output-to "resources/public/js/compiled/app.js"
                                   :optimizations :advanced
                                   :closure-defines {name-bazaar.ui.config.environment "prod"}
                                   :pretty-print false
                                   :pseudo-names false}}

                       ;; Testing on server-side
                       {:id "server-tests"
                        :source-paths ["src" "test"]
                        :figwheel true
                        :compiler {:main "server.run-tests"
                                   :output-to "server-tests/server-tests.js",
                                   :output-dir "server-tests",
                                   :target :nodejs,
                                   :optimizations :none,
                                   :verbose false
                                   :source-map true}}]})

