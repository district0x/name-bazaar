pragma solidity ^0.4.17;

/**
 * @title BuyNowOffering
 * @dev Contains logic for BuyNowOffering. This contract will be deployed only once, while users will create
 * many instances of Forwarder via BuyNowOfferingFactory, which will serve as a proxy pointing this contract.
 * This way code logic for this offering won't be duplicated on blockchain.
 */

import "Offering.sol";

contract BuyNowOffering is Offering {

    /**
    * @dev Exchanges funds of new owner for ownership of ENS name owner
    * msg.value must exactly equal to offering price
    */
    function buy()
        public
        payable
    {
        require(msg.value == offering.price);
        offering.originalOwner.transfer(offering.price);
        transferOwnership(msg.sender);
    }

    /**
    * @dev Changes settings for BuyNowOffering
    * Can be executed only by original owner
    * Can't be executed after ownership was already transferred to a new owner
    * @param _price uint New price of the offering
    */
    function setSettings(uint _price)
        public
        onlyOriginalOwner
        onlyWithoutNewOwner
    {
        super.doSetSettings(_price);
        fireOnChanged("setSettings");
    }
}
