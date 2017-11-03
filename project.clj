(defproject name-bazaar "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 ;[district0x "0.1.10"]
                 [cljs-http "0.1.43"]
                 [cljs-web3 "0.19.0-0-7"]
                 [cljsjs/eccjs "0.3.1-0"]
                 [cljsjs/prop-types "15.5.10-0"]
                 [cljsjs/react "15.6.1-2"]
                 [cljsjs/react-datepicker "0.55.0-0"]
                 [cljsjs/react-dom "15.6.1-2"]
                 [cljsjs/react-dom-server "15.6.1-2"]
                 [cljsjs/react-infinite "0.12.1-0"]
                 [day8.re-frame/async-flow-fx "0.0.8"]
                 [day8.re-frame/forward-events-fx "0.0.5"]
                 [honeysql "0.9.1"]
                 [lein-doo "0.1.8"]
                 [medley "1.0.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [print-foo-cljs "2.0.3"]
                 [re-frame "0.9.4"]
                 [re-frisk "0.4.4"]
                 [soda-ash "0.4.0"]

                 ;; d0xINFRA temporary here
                 [akiroz.re-frame/storage "0.1.2"]
                 [bidi "2.1.2"]
                 [camel-snake-kebab "0.4.0"]
                 [cljs-ajax "0.7.2"]
                 [cljs-node-io "0.5.0"]
                 [cljsjs/bignumber "2.1.4-1"]
                 [cljsjs/react-flexbox-grid "1.0.0-0"]
                 [cljsjs/react-highlight "1.0.5-0"]
                 [cljsjs/react-truncate "2.0.3-0"]
                 [cljsjs/react-ultimate-pagination "0.8.0-0"]
                 [com.andrewmcveigh/cljs-time "0.5.1"]
                 [com.cognitect/transit-cljs "0.8.243"]
                 [com.taoensso/encore "2.92.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [madvas/cemerick-url-patched "0.1.2-SNAPSHOT"] ;; Temporary until cemerick merges PR26
                 ;[com.cemerick/url "0.1.1"]
                 [day8.re-frame/http-fx "0.1.4"]
                 [kibu/pushy "0.3.8"]
                 [madvas.re-frame/google-analytics-fx "0.1.0"]
                 [madvas.re-frame/web3-fx "0.2.1"]]

  :exclusions [[com.taoensso/encore]
               [org.clojure/clojure]
               [org.clojure/clojurescript]]

  :plugins [[lein-auto "0.1.2"]
            [lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.14"]
            [lein-shell "0.5.0"]
            [lein-doo "0.1.7"]
            [lein-npm "0.6.2"]
            [lein-pdo "0.1.1"]]

  :npm {:dependencies [[cors "2.8.4"]
                       [eccjs "0.3.1"]
                       [eth-ens-namehash "2.0.0"]
                       [ethereumjs-testrpc "4.1.3"]
                       [express "4.15.3"]
                       [solc "0.4.13"]
                       [source-map-support "0.4.0"]
                       [sqlite3 "3.1.8"]
                       [web3 "0.19.0"]
                       [ws "2.0.1"]
                       [xhr2 "0.1.4"]
                       [semantic-ui "2.2.13"]
                       [chalk "2.3.0"] ;;ANSI colors for logging
                       ]
        :devDependencies [[karma "1.5.0"]
                          [karma-chrome-launcher "2.0.0"]
                          [karma-cli "1.0.1"]
                          [karma-cljs-test "0.1.0"]
                          [karma-safari-launcher "1.0.0"]]}

  :doo {:paths {:karma "./node_modules/karma/bin/karma"}}

  :min-lein-version "2.5.3"

  :source-paths ["src" "test"]
  
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:server-port 4544
             :ring-handler dev-server/web-handler
             :css-dirs ["resources/public/css"]}

  :repl-options {:timeout 120000}
  :auto {"compile-solidity" {:file-pattern #"\.(sol)$"
                             :paths ["resources/public/contracts/src"]}}

  :aliases {"compile-solidity" ["shell" "./compile-solidity.sh"]
            "clean-prod-server" ["shell" "rm" "-rf" "server"]
            "watch-css" ["shell" "./semantic.sh" "watch"]
            "build-css" ["shell" "./semantic.sh" "build-css"]
            "build-prod-server" ["do" ["clean-prod-server"] ["cljsbuild" "once" "server"]]
            "build-prod-ui" ["do" ["clean"] ["cljsbuild" "once" "min"]]
            "build-prod" ["pdo" ["build-prod-server"] ["build-prod-ui"] ["build-css"]]}

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [binaryage/devtools "0.9.7"]
                                  [com.cemerick/piggieback "0.2.2"]
                                  [compojure "1.6.0"]                                  
                                  [ring/ring-defaults "0.2.0"]
                                  [ring/ring-core "1.6.0"]
                                  [figwheel-sidecar "0.5.14" :exclusions [org.clojure/core.async]]
                                  [org.clojure/tools.nrepl "0.2.13"]]
                   :source-paths ["dev" "src"]
                   :resource-paths ["resources"]}}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/name_bazaar/ui" "src/name_bazaar/shared"
                                       "src/district0x/ui" "src/district0x/shared"]
                        :figwheel {:on-jsload "name-bazaar.ui.core/mount-root"}
                        :compiler {:main "name-bazaar.ui.core"
                                   :output-to "resources/public/js/compiled/app.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :asset-path "js/compiled/out"
                                   :source-map-timestamp true
                                   :preloads [print.foo.preloads.devtools]
                                   :closure-defines {goog.DEBUG true
                                                     name-bazaar.ui.db.environment "dev"
                                                     district0x.ui.history.pushroute-hosts "localhost"
                                                     name-bazaar.ui.db.log-level "debug"}
                                   :external-config {:devtools/config {:features-to-install :all}}}}
                       {:id "dev-server"
                        :source-paths ["src/name_bazaar/server" "src/name_bazaar/shared"
                                       "src/district0x/server" "src/district0x/shared"]
                        :figwheel {:on-jsload "name-bazaar.server.dev/on-jsload"}
                        :compiler {:main "name-bazaar.server.dev"
                                   :output-to "dev-server/name-bazaar.js",
                                   :output-dir "dev-server",
                                   :target :nodejs,
                                   :optimizations :none,
                                   :closure-defines {goog.DEBUG true}
                                   :source-map true}}
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
                       {:id "min"
                        :source-paths ["src"]
                        :compiler {:main "name-bazaar.ui.core"
                                   :output-to "resources/public/js/compiled/app.js"
                                   :optimizations :advanced
                                   :closure-defines {goog.DEBUG false
                                                     name-bazaar.ui.db.environment "prod"
                                                     district0x.ui.history.pushroute-hosts "beta.namebazaar.io,namebazaar.io"}
                                   :pretty-print false
                                   :pseudo-names false}}
                       {:id "server-tests"
                        :source-paths ["src/name_bazaar/server" "src/name_bazaar/shared"
                                       "src/district0x/server" "src/district0x/shared"
                                       "test/server"]
                        :figwheel true
                        :compiler {:main "server.run-tests"
                                   :output-to "server-tests/server-tests.js",
                                   :output-dir "server-tests",
                                   :target :nodejs,
                                   :optimizations :none,
                                   :verbose false
                                   :source-map true}}
                       {:id "browser-tests"
                        :source-paths ["src/name_bazaar/ui" "src/name_bazaar/shared"
                                       "src/district0x/ui" "src/district0x/shared"
                                       "test/browser"]
                        :compiler {:output-to "browser-tests/browser-tests.js",
                                   :output-dir "browser-tests",
                                   :main browser.run-tests
                                   :optimizations :none}}]})
