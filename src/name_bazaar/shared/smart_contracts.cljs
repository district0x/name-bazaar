(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:instant-buy-offering-factory
 {:name "InstantBuyOfferingFactory",
  :address "0x7ce0edf0420f4d91bb194f1ff9b5a457cc87c2c7"},
 :instant-buy-offering-library
 {:name "InstantBuyOfferingLibrary",
  :address "0xdd6d6fe075a10db018442fa152591d2ad6991ca0"},
 :english-auction-offering-library
 {:name "EnglishAuctionOfferingLibrary",
  :address "0x923df140ae238fefcd70ff0167390aa5baa42aaa"},
 :english-auction-offering-factory
 {:name "EnglishAuctionOfferingFactory",
  :address "0x6d51c5a57f684989b38b3c73c164077cd47e8241"},
 :english-auction-offering
 {:name "EnglishAuctionOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :instant-buy-offering
 {:name "InstantBuyOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0x43662f322fb3891d186e8b0c06d84b00dcc895d0"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0x26128b5c620d85aa5c42e53db2e7c08a631c8ab1"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x0000000000000000000000000000000000000000"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x861034996b07f875bbc15e1c009ed0d8785165e2"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0x5d0d7b382377f9328ed1fdc3fc06d3dfac2c4472"}})