(ns district0x.shared.key-utils
  (:require
   [cljsjs.eccjs :as eccjs]
   [goog.crypt.base64 :as base64]))

(defonce default-keypair
  {:private-key "25615758538fef2b8a65aa7146c273fb17c03b0d73642feac250b7e79d8f06793eb"
   :public-key "256ebc161b4751583b3718e77bd5bff97027c607daa553385094ce9410ebe7531b422f7b5f2702ba80b53092024ccc63c4a8c96ba7387e063500a58cce0c7b3a3ee"})

;; TODO: nice if this was a dynamic var instead of an atom
(def ^private ^{:doc "Currently loaded keypair"} *keypair* (atom nil))

(defn get-current-keypair
  []
  "Get the currently loaded keypair"
  (deref *keypair*))

(defn set-current-keypair!
  [keypair]
  "Set the keypair"
  (reset! *keypair* keypair))

(defn generate-keypair []
  (let [keypair (js->clj (.generate js/ecc (.-ENC_DEC js/ecc) 256))]
    {:public-key (get keypair "enc")
     :private-key (get keypair "dec")}))

(defn encrypt [public-key content]
  (.encrypt js/ecc public-key content))

(defn decrypt [private-key content]
  (.decrypt js/ecc private-key content))

(defn encode-base64 [s]
  (base64/encodeString s))

(defn decode-base64 [s]
  (base64/decodeString s))
