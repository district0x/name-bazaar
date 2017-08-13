pragma solidity ^0.4.14;

import "OfferingRegistry.sol";
import "OfferingFactory.sol";
import "BuyNowOffering.sol";
import "strings.sol";

contract BuyNowOfferingFactory is OfferingFactory {
    using strings for *;

    function BuyNowOfferingFactory(
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
        address newOffering = new BuyNowOffering(
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

