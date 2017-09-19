(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0x69b58e9154d3c5430a58466c8d536de68cce555e"},
 :buy-now-offering-library
 {:name "BuyNowOfferingLibrary",
  :address "0xcbc567665551fd1056d300e85a42ef33fb0fcfe8"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0xcb57a07ea1e7312be6cf14d417b1bbb6a81eab85"},
 :registrar
 {:name "Registrar",
  :address "0x542d337975c2f9b436621024df371244bd53a0af"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :auction-offering-library
 {:name "AuctionOfferingLibrary",
  :address "0x2ecef7c1114d90328c74661c35b7d5846bea4f92"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0x44e134c9cbc6547699fbb5785a4ae85120116459"},
 :mock-registrar
 {:name "MockRegistrar",
  :address "0x0f4fbbb8c7f4eebb795de2c94e63b61c05a2f198"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0xc500fa5e261de8aebe962b6ba1829831f80a1823"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0xd82fcd42957fdf673fb051f258d7a1dc6dfa9a0d"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x9020ac9815d5c6777787afc3ae96e1c7bb967a31"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0xf2e18734c33ebecc07243860cf4476d368245495"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x0000000000000000000000000000000000000000"}})