(ns name-bazaar.shared.smart-contracts)

(def smart-contracts
  {:auction-offering-factory
   {:name "AuctionOfferingFactory",
    :address "0x43f1481ca7876d49ac189d16e317a398b22a46d6"},
   :buy-now-offering-factory
   {:name "BuyNowOfferingFactory",
    :address "0xeb84d5ab9c659779526e76e597c90467de468289"},
   :name-bazaar-registrar
   {:name "NameBazaarRegistrar",
    :address "0x5ed7c4d6ba0f58e4ac0eacb4bbec98166018f3ea"},
   :buy-now-offering
   {:name "BuyNowOffering",
    :address "0x8c8a52496b94581f1a8ce5d740ab02bcbb8a4d5a"},
   :reverse-registrar
   {:name "ReverseRegistrar",
    :address "0x9062C0A6Dbd6108336BcBe4593a3D1cE05512069"},
   :deed
   {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
   :public-resolver
   {:name "PublicResolver",
    :address "0x5FfC014343cd971B7eb70732021E26C35B744cc4"},
   :ens
   {:name "ENSRegistry", :address "0x5c0a54e5d85d7422f47c0a4d877fe984aac1bf12"},
   :offering-registry
   {:name "OfferingRegistry",
    :address "0xe22a7319429be44c75d15329c65643c19605c8d1"},
   :district0x-emails
   {:name "District0xEmails",
    :address "0xbf8eee16a979d950926b300af61f4b0abf67277c"},
   :offering-requests
   {:name "OfferingRequests",
    :address "0x7c2d5f61f0c9ce49cb2c8d436c187f7540beb286"},
   :auction-offering
   {:name "AuctionOffering",
    :address "0xc49fab73e030dcbf31058ce6da263e63277b7489"}})