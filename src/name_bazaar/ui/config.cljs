(ns name-bazaar.ui.config)

;; TODO currently this is configuration in source code
;; you need to rebuild UI for each new configuration
;; (e.g. change this file then build new docker UI image)
;; we should make UI configurable on the fly
(goog-define environment "qa")

(def development-config
  {:debug? true
   :logging {:level :debug
             :console? true}
   :pushroute-hosts "localhost"
   :infura-id "fd0074468fd64e36b495c846a26a3f9d" ;; this is for wallet-connect only, which will not work when running with ganache
   :fallback-web3-url "https://goerli.infura.io/"
   :load-node-addresses? true
   :root-url "http://0.0.0.0:4544"
   :server-url "http://localhost:6200"})

(def qa-development-config
  {:debug? true
   :logging {:level :debug
             :console? true}
   :pushroute-hosts "localhost"
   :infura-id "fd0074468fd64e36b495c846a26a3f9d" ;; this is for wallet-connect only, which will not work when running with ganache
   :fallback-web3-url "https://goerli.infura.io/"
   :load-node-addresses? true
   :root-url "http://0.0.0.0:4544"
   :server-url "http://localhost:6200"})

(def qa-config
  {:debug? true
   :logging {:level :warn
             :console? true}
   :pushroute-hosts "namebazaar.qa.district0x.io"
   :infura-id "fd0074468fd64e36b495c846a26a3f9d" ;; this is for wallet-connect only, which will not work when running with ganache
   :fallback-web3-url "https://goerli.infura.io/"
   :load-node-addresses? true
   :root-url "https://namebazaar.qa.district0x.io"
   :server-url "https://api.namebazaar.qa.district0x.io"})

(def preprod-config
  {:logging {:level :debug}
   :pushroute-hosts "namebazaar.io"
   :infura-id "0ff2cb560e864d078290597a29e2505d"
   :fallback-web3-url "https://mainnet.infura.io/"
   :load-node-addresses? true
   :root-url "https://namebazaar.preprod.district0x.io"
   :server-url "https://api.namebazaar.preprod.district0x.io"})

(def production-config
  {:logging {:level :warn
            ;;  :sentry {:dsn "https://597ef71a10a240b0949c3b482fe4b9a4@sentry.io/1364232"}
            }
   :pushroute-hosts "namebazaar.io"
   :infura-id "0ff2cb560e864d078290597a29e2505d"
   :fallback-web3-url "https://mainnet.infura.io/"
   :load-node-addresses? true
   :root-url "https://namebazaar.io"
   :server-url "https://api.namebazaar.io"}
  ;;  :server-url "prod_namebazaar-server:3000"}
   )


(def config
  (condp = environment
    "prod" production-config
    "preprod" preprod-config
    "qa"  qa-config
    "dev" development-config))
