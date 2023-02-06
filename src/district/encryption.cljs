(ns district.encryption
  (:require
    ["eccjs" :as ecc]
    [goog.crypt.base64 :as base64]))

(defn generate-keypair []
  (let [keypair (js->clj (.generate ecc (.-ENC_DEC ecc) 256))]
    {:public-key (get keypair "enc")
     :private-key (get keypair "dec")}))

(defn encrypt [public-key content]
  (.encrypt ecc public-key content))

(defn decrypt [private-key content]
  (.decrypt ecc private-key content))

(defn encode-base64 [s]
  (base64/encodeString s))

(defn decode-base64 [s]
  (base64/decodeString s))

(defn encrypt-encode [public-key content]
  (try  (->> content
             (encrypt public-key)
             (encode-base64))
        (catch js/Object e
          (throw
           (ex-info "Exception encrypting content"
                    {:cause (ex-message e)})))))

(defn decode-decrypt [private-key content]
  (try
    (->> content
         (decode-base64)
         (decrypt private-key))
    (catch js/Object e
      (throw
       (ex-info "Exception decoding content"
                {:cause (ex-message e)})))))
