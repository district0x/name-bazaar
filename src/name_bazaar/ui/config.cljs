(ns name-bazaar.ui.config
  (:require [name-bazaar.shared.utils :refer [get-environment]]))

;; TODO : pushroute hosts

(def development-config
  {:logging {:level :debug
             :console? true
             :file-path "/home/filip/Dropbox/district0x/name-bazaar.dev.log"
             :sentry {:dsn "https://597ef71a10a240b0949c3b482fe4b9a4@sentry.io/1364232"}}

   :node-url "http://localhost:8549"
   :load-node-addresses? true
   :root-url "http://0.0.0.0:4544"
   :server-url "http://localhost:6200"

   })

(def qa-config
  {:logging {:level :warn
             :sentry {:dsn "https://597ef71a10a240b0949c3b482fe4b9a4@sentry.io/1364232"}}
   :node-url "http://localhost:8549"
   :load-node-addresses? true
   :root-url "https://beta.namebazaar.io"
   :server-url "http://localhost:6200"

   })

(def production-config
  {:logging {:level :warn
             :sentry {:dsn "https://597ef71a10a240b0949c3b482fe4b9a4@sentry.io/1364232"}}
   :node-url "https://mainnet.infura.io/"
   :load-node-addresses? false
   :root-url "https://namebazaar.io"
   :server-url "https://api.namebazaar.io"
   })

(def config
  (condp = (get-environment)
    "prod" production-config
    "qa"   qa-config
    "dev"  development-config))
