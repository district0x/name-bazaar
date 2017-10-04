(ns server.district0x.encryption-utils-tests
  (:require [cljs.test :refer [deftest is testing run-tests]]
            [district0x.server.state :as state]
            [district0x.shared.encryption-utils :as encryption-utils]))

(defn contains-many? [m & ks]
  (every? #(contains? m %) ks))

(deftest encyption-utils-tests
  (testing "keypair generation test."
    (is (contains-many? (encryption-utils/generate-keypair) :public-key :private-key)))
  
  (testing "content decryption/encryption test."
    (let [keypair (encryption-utils/generate-keypair)
          content "top secret"
          base64-encrypted-content (->> content
                                        (encryption-utils/encrypt (:public-key keypair)) 
                                        (encryption-utils/encode-base64))
          decoded-content (->> base64-encrypted-content
                               (encryption-utils/decode-base64)
                               (encryption-utils/decrypt (:private-key keypair)))]
      (is (= content decoded-content)))))

