(ns browser.key-utils-tests
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [district0x.shared.key-utils :as key-utils]))

(defn contains-many? [m & ks]
  (every? #(contains? m %) ks))

(deftest key-utils-tests
  (testing "keypair generation test."
    (is (contains-many? (key-utils/generate-keypair) :public-key :private-key)))

  (testing "content decryption/encryption test."
    (let [keypair (key-utils/generate-keypair)
          content "top secret"
          base64-encrypted-content (->> content
                                        (key-utils/encrypt (:public-key keypair)) 
                                        (key-utils/encode-base64))
          decoded-content (->> base64-encrypted-content
                               (key-utils/decode-base64)
                               (key-utils/decrypt (:private-key keypair)))]
         (is (= content decoded-content)))))

