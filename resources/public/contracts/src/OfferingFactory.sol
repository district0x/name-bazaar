pragma solidity ^0.4.14;

import "ens/ENS.sol";
import "OfferingRegistry.sol";
import "OfferingRequestsAbstract.sol";
import "strings.sol";

contract OfferingFactory {
    using strings for *;

    ENS public ens;
    OfferingRegistry public offeringRegistry;
    OfferingRequestsAbstract public offeringRequests;
    address public emergencyMultisig;

    function OfferingFactory(
        address _ens,
        address _offeringRegistry,
        address _offeringRequests,
        address _emergencyMultisig
    ) {
        ens = ENS(_ens);
        offeringRegistry = OfferingRegistry(_offeringRegistry);
        offeringRequests = OfferingRequestsAbstract(_offeringRequests);
        emergencyMultisig = _emergencyMultisig;
    }

    function registerOffering(bytes32 node, address newOffering, uint version)
        internal
    {
        require(ens.owner(node) == msg.sender);
        offeringRegistry.addOffering(newOffering, node, msg.sender, version);
        offeringRequests.clearRequests(node);
    }

    function namehash(string name) internal returns(bytes32) {
        var nameSlice = name.toSlice();

        if (nameSlice.len() == 0) {
            return bytes32(0);
        }

        var label = nameSlice.split(".".toSlice()).toString();
        return sha3(namehash(nameSlice.toString()), sha3(label));
    }
}