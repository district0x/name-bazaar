(ns name-bazaar.smart-contracts) 

(def smart-contracts 
{:instant-buy-offering-factory
 {:name "InstantBuyOfferingFactory",
  :address "0x794e077c12cff61ddef7d9bdac6b61cde7b2cc5d"},
 :instant-buy-offering-library
 {:name "InstantBuyOfferingLibrary",
  :address "0xd373a66882a91539ec716c81ab01c662d6f3921d"},
 :english-auction-offering-library
 {:name "EnglishAuctionOfferingLibrary",
  :address "0x191bb38bf8ad222275b5185ddb90f79a10accf32"},
 :english-auction-offering-factory
 {:name "EnglishAuctionOfferingFactory",
  :address "0xf8ac10a379ae172ae5b8bc00159a95b87ec82614"},
 :english-auction-offering
 {:name "EnglishAuctionOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :instant-buy-offering
 {:name "InstantBuyOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0x1c4e6382c03bd43518323d3d48c6feaaa9ffe563"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0x2686c48bad5869f305c57fb623c4f71c639d63a6"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x896b97c5f8cc98627d0907eff8687c485b3d45c4"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0xdc9758adc62e9a3b719329bc374ae7d270ddce98"}})