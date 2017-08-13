pragma solidity ^0.4.14;

import "OfferingRegistry.sol";
import "OfferingFactory.sol";
import "InstantBuyOffering.sol";
import "strings.sol";

contract InstantBuyOfferingFactory is OfferingFactory {
    using strings for *;

    function InstantBuyOfferingFactory(
        address registrar,
        address offeringRegistry,
        address offeringRequests,
        address emergencyMultisig
    )
        OfferingFactory(registrar, offeringRegistry, offeringRequests, emergencyMultisig)
    {
    }

    function createOffering(
        string name,
        uint price
    ) {
        var node = namehash(name);
        var labelHash = getLabelHash(name);
        address newOffering = new InstantBuyOffering(
            offeringRegistry,
            registrar,
            node,
            name,
            getLabelHash(name),
            msg.sender,
            emergencyMultisig,
            price
        );
        registerOffering(node, labelHash, newOffering, 1);
    }
}

