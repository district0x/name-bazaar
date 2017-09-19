(ns district0x.server.key-utils
  (:require
   [cljsjs.eccjs :as eccjs]))

(defn generate-keypair []
  (-> (.generate js/ecc (.-ENC_DEC js/ecc) )
      (js->clj :keywordize-keys true)))
