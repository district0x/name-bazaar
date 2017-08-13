(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:instant-buy-offering-factory
 {:name "InstantBuyOfferingFactory",
  :address "0x625f35e9a343db0814b3c8f6c0d04d6756020572"},
 :registrar
 {:name "MockRegistrar",
  :address "0x9abf2fb86b7f4d3fb59f262061d27efd7457f7b6"},
 :instant-buy-offering-library
 {:name "InstantBuyOfferingLibrary",
  :address "0x1ab4e18108fe86271e666fa0630106aa3f039df4"},
 :english-auction-offering-library
 {:name "EnglishAuctionOfferingLibrary",
  :address "0x87c7ba8246b54a0f1b2c427730d1dad4c7d37c19"},
 :english-auction-offering-factory
 {:name "EnglishAuctionOfferingFactory",
  :address "0x740bf98063db8876db69058c75127e0b30019396"},
 :english-auction-offering
 {:name "EnglishAuctionOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :instant-buy-offering
 {:name "InstantBuyOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0x0e09bcb071ce80de476d52f3be016812ff2733db"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0x63649e469210d1e042967a88a53d69b111f513da"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x0000000000000000000000000000000000000000"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0xf38e2120e2f6534034edba6057f1a82c7e512e08"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0xe37332440658f72aa7b935c6c6dd3d6cb0983af7"}})