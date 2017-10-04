(ns server.district0x.encryption-utils-tests
  (:require [cljs.test :refer [deftest is testing run-tests]]
            [district0x.shared.config :as config]
            [district0x.shared.encryption-utils :as encryption-utils]))

(defn contains-many? [m & ks]
  (every? #(contains? m %) ks))

(deftest encyption-utils-tests
    (testing "content decryption/encryption test."
    (let [keypair (select-keys config/default-config [:public-key :private-key])
          content "top secret"
          base64-encrypted-content (->> content
                                        (encryption-utils/encrypt (:public-key keypair)) 
                                        (encryption-utils/encode-base64))
          decoded-content (->> base64-encrypted-content
                               (encryption-utils/decode-base64)
                               (encryption-utils/decrypt (:private-key keypair)))]
         (is (= content decoded-content)))))

