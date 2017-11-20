(ns browser.encryption-tests
  (:require [cljs.test :refer [deftest is testing run-tests]]
            [cljsjs.web3]
            [cljs-web3.core :as web3]
            [district0x.ui.utils :as d0x-ui-utils]))

(deftest encryption-tests
  (testing "solidity-sha3 tests."
    (let [label "testit"
          label-hash (web3/sha3 label)
          expected-label-hash "0x7134ab38219c85a73cbce815e6604eab296ad8f434800f5668adccf3a5f0b764"
          address "0x57ea6759480ef6705f27c5cd198fca60bf7b82d1"
          value 0.01
          value-wei (web3/to-wei value :ether)
          expected-value-wei "10000000000000000"
          salt "secret"
          hash-salt (web3/sha3 salt)
          expected-hash-salt "0x65462b0520ef7d3df61b9992ed3bea0c56ead753be7c8b3614e0ce01e4cac41b"
          sealed-bid (district0x-ui-utils/solidity-sha3 label-hash address 10000000000000000 hash-salt)
          expected-sealed-bid "0xd6673ac22a73792d97d4b7e1b24a6d02b6eeefed5a679f3f9b6c06fa7ebca261"]
      ;; values from : http://web3js.readthedocs.io/en/1.0/web3-utils.html#soliditysha3
      (is (= "0x61c831beab28d67d1bb40b5ae1a11e2757fa842f031a2d0bc94a7867bc5d26c2" (solidity-sha3 234)))
      (is (= "0x661136a4267dba9ccdf6bfddb7c00e714de936674c4bdb065a531cf1cb15c7fc" (solidity-sha3 "Hello!%")))
      (is (= expected-label-hash label-hash))
      (is (= expected-value-wei value-wei))
      (is (= expected-hash-salt hash-salt))
      (is (= expected-sealed-bid sealed-bid)))))
