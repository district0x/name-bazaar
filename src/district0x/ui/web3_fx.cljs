(ns district0x.ui.web3-fx
  (:require
    [re-frame.core :as re-frame :refer [reg-fx dispatch]]))


(defn supports-ethereum-provider?
  "Determines whether the browser has the window.ethereum object. All
  browsers are encouraged to implement this object with the method
  `.enable` to invoke an authorization dialog as defined by EIP-1102."
  []
  (some-> js/window (aget "ethereum") (aget "send")))


(defn authorize []
  (let [eth-send (aget js/window "ethereum" "send")]
    (eth-send "eth_requestAccounts")))


(defn web3-legacy? []
  (not (some-> js/window (aget "ethereum"))))
