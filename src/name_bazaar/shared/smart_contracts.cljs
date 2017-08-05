(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:instant-buy-offering-factory
 {:name "InstantBuyOfferingFactory",
  :address "0xb70070eeafd33640f0371fbf19752b00cd013adf"},
 :instant-buy-offering-library
 {:name "InstantBuyOfferingLibrary",
  :address "0xd2dd56ccf06171f756e0fdefc6d465cbdc1db24b"},
 :english-auction-offering-library
 {:name "EnglishAuctionOfferingLibrary",
  :address "0x93a10aa22741c984e5378d82245ae4c94c9e9b84"},
 :english-auction-offering-factory
 {:name "EnglishAuctionOfferingFactory",
  :address "0xe905cb162143a400d438ecc6ea81d692714653be"},
 :english-auction-offering
 {:name "EnglishAuctionOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :instant-buy-offering
 {:name "InstantBuyOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0x8d821b4cacaab8e099c6d00208a435f59a16d141"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0x93f8d18f8fdd9634379efaad3d8c7239d5dca26e"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x0000000000000000000000000000000000000000"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x9809c418aed8aee7d7683f31ab55595c1fdc8426"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0xc7305cb72250429edfbaa683f523de9813317a69"}})