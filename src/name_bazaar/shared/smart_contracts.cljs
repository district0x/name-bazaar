(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0x287631d399e08b917403986a949084877d2f39a5"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0x6815ad140b152157634be9ffc8c51d57e02da38e"},
 :registrar
 {:name "Registrar",
  :address "0x6090A6e47849629b7245Dfa1Ca21D94cd15878Ef"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0xaf8a60b8d53f0563abadc3b33d0bf28517a6b696"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0x314159265dD8dbb310642f98f50C066173C1259b"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0x34e400a8b4da8a23b5eaf81b46d3a887669a45b9"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x5065ef0724b76421aeaafa87ba752d6c5d5499b5"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x016bdfcf49ecdd9592e493cd4a75048ce09d6d75"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x24b6ee04740ce5051539117409660f15e29ea329"}})