pragma solidity ^0.4.11;

import "OfferingRegistry.sol";
import "OfferingFactory.sol";
import "InstantBuyOffering.sol";
import "strings.sol";

contract InstantBuyOfferingFactory is OfferingFactory {
    using strings for *;

    function InstantBuyOfferingFactory(address ens, address offeringRegistry, address offeringRequests)
        OfferingFactory(ens, offeringRegistry, offeringRequests)
    {
    }

    function createOffering(
        string name,
        uint price
    ) {
        bytes32 node = namehash(name);
        address newOffering = new InstantBuyOffering(ens, node, name, msg.sender, price);
        registerOffering(node, newOffering);
    }
}


