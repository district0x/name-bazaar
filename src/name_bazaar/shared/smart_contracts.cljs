(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0x244a1e48598d2cd99a2b4867e43753bba48b981a"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0x865d17af3700446afd6f9dc3842c1b4555c43884"},
 :registrar
 {:name "Registrar",
  :address "0xc7ce3fd7e0e71e488f8b23015a27c0a2b7266d60"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x8bf3f876e857efdf97532f30d212979cb2e92ae4"},
 :reverse-registrar
 {:name "ReverseRegistrar",
  :address "0x9062C0A6Dbd6108336BcBe4593a3D1cE05512069"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :public-resolver
 {:name "PublicResolver",
  :address "0x5FfC014343cd971B7eb70732021E26C35B744cc4"},
 :ens
 {:name "ENS", :address "0x20da1b15532029c99659a8865405fa46a084a034"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0xb0e08ecc6f2aeb94f3cbedb0acd4a542daef42cf"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x099fe7b4f89b795786bf4f8c5d94d822b0190453"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0xc7bd4b27a0bf458d5bc0c0f728b93e5cbbcf0bdd"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x4e2353c34f06157a744a42eb9a1e4629002e5a18"}})