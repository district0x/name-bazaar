(ns browser.encryption-tests
  (:require [cljs.test :refer [deftest is testing run-tests]]
            [district0x.shared.encryption-utils :as encryption-utils]))

(deftest encryption-tests
    
  (testing "content encryption test."
    (let [keypair (encryption-utils/generate-keypair)
          content "top-secret@district0x.io"
          base64-encrypted-content (->> content
                                        (encryption-utils/encrypt (:public-key keypair)) 
                                        (encryption-utils/encode-base64))
          decoded-content (->> base64-encrypted-content
                               (encryption-utils/decode-base64)
                               (encryption-utils/decrypt (:private-key keypair)))]
      (is (= content decoded-content)))))
