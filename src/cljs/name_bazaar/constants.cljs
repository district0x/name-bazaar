(ns name-bazaar.constants)

(def contracts-version "1.0.0")

(def contracts-method-args
  {:instant-buy-offering-factory/create-offering [:instant-buy-offering-factory/name
                                                  :instant-buy-offering-factory/price]
   :english-auction-offering-factory/create-offering [:english-auction-offering-factory/name
                                                      :english-auction-offering-factory/start-price
                                                      :english-auction-offering-factory/start-time
                                                      :english-auction-offering-factory/end-time
                                                      :english-auction-offering-factory/extension-duration
                                                      :english-auction-offering-factory/extension-trigger-duration
                                                      :english-auction-offering-factory/min-bid-increase]})

(def contracts-method-wei-args #{:instant-buy-offering-factory/price
                                 :english-auction-offering-factory/start-price
                                 :english-auction-offering-factory/min-bid-increase})
