(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0xa22d3841f9eaaa0b14e92e9bf0bc3e4cc895dc65"},
 :buy-now-offering-library
 {:name "BuyNowOfferingLibrary",
  :address "0x1e9130d15451645322debd69a8030ebc4c97988e"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0xce07d49730f46353430da95606a4f67b0d683fb9"},
 :registrar
 {:name "Registrar",
  :address "0x542d337975c2f9b436621024df371244bd53a0af"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :auction-offering-library
 {:name "AuctionOfferingLibrary",
  :address "0x0298077c30bb60886c0274c1e290a9e7ae3825c9"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0x8837154a23d7f64c59b1acb655aec0f855e9db4a"},
 :mock-registrar
 {:name "MockRegistrar",
  :address "0xe25ea9340fcc24387c77834c255db956e0120fb6"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0xa67042ee3fc8600af4d713d65e5f52cb0575c593"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x0000000000000000000000000000000000000000"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x8e649118a71e7106defc93ca19c9132100152304"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0x491dea27c66ad6d67759b0ad42ab7a7b2558ff90"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x0000000000000000000000000000000000000000"}})