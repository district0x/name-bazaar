(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0xc7276737e39235ecd98b080ff0ad766408804e7e"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0x7f4475a4c6899588cee51925da7531538e22fc3c"},
 :registrar
 {:name "Registrar",
  :address "0x0c0747e333497fe9711301761be4b5d64000da87"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0xd7f25c4bdcbf5ed3b7a9f28581fd426671bb0474"},
 :reverse-registrar
 {:name "ReverseRegistrar",
  :address "0x9062C0A6Dbd6108336BcBe4593a3D1cE05512069"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :public-resolver
 {:name "PublicResolver",
  :address "0x5FfC014343cd971B7eb70732021E26C35B744cc4"},
 :ens
 {:name "ENS", :address "0x62c88b9c754eef25705ba2afbdb175ab37a746b5"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0x5a6136ce61837400067880fd5724eeafba36af21"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0xd0e47e4fea69ed90a578146633be0e16483beab5"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x89238e5f0a660dea8a08b0aacaad952e4dcaf941"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x72640d2939d01f96df80911fd1277698ac4f1241"}})