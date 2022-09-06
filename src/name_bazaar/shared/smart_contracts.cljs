(ns name-bazaar.shared.smart-contracts)
    (def smart-contracts
      {:ens {:name "ENSRegistry" :address "0x7CF6296490e6048409c20d65f1a453A2EecfBcC0"}
       :eth-registrar {:name "NameBazaarDevRegistrar" :address "0x252b9609781923b58C9e44C22b49160A3F503Af3"}
       :offering-registry {:name "OfferingRegistry" :address "0xDe143e7eE5CD9488EFF68D386304335bc2806269"}
       :buy-now-offering {:name "BuyNowOffering" :address "0x369952536Bd65A096755D1494038d6074A816943"}
       :buy-now-offering-factory {:name "BuyNowOfferingFactory" :address "0xE49b5Ca05310d21EDB1A57C5b9B6695E77903508"}
       :auction-offering {:name "AuctionOffering" :address "0x220aFd01F53887d1DD03052a12c720F58C111675"}
       :auction-offering-factory {:name "AuctionOfferingFactory" :address "0x84e6E9d2bAC10C3d335Abf81E69ac9Ea42efec7C"}
       :district0x-emails {:name "District0xEmails" :address "0x7364f83085A2c20263830cB6EEc5901C6EdDaA46"}
       :reverse-name-resolver {:name "NamebazaarDevNameResolver" :address "0x0FDB292129057eeA228B235f5E5c19d59a007404"}
       :public-resolver {:name "NamebazaarDevPublicResolver" :address "0xF9Ee67479FEDBE5601B8Af4237962d6eF7d58DEC"}
       :reverse-registrar {:name "NamebazaarDevReverseRegistrar" :address "0x7F2703aB51261651f00Fe51E2A65F2F9bFa564C1"}})