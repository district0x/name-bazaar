pragma solidity ^0.4.14;

import "ens/HashRegistrarSimplified.sol";
import "OfferingRegistry.sol";
import "OfferingRequestsAbstract.sol";
import "strings.sol";

contract OfferingFactory {
    using strings for *;

    Registrar public registrar;
    OfferingRegistry public offeringRegistry;
    OfferingRequestsAbstract public offeringRequests;
    address public emergencyMultisig;

    function OfferingFactory(
        address _registrar,
        address _offeringRegistry,
        address _offeringRequests,
        address _emergencyMultisig
    ) {
        registrar = Registrar(_registrar);
        offeringRegistry = OfferingRegistry(_offeringRegistry);
        offeringRequests = OfferingRequestsAbstract(_offeringRequests);
        emergencyMultisig = _emergencyMultisig;
    }

    function registerOffering(bytes32 node, bytes32 labelHash, address newOffering, uint version)
        internal
    {
        require(registrar.ens().owner(node) == msg.sender);
        if (node == sha3(registrar.rootNode(), labelHash)) {
            address deed;
            (,deed,,,) = registrar.entries(labelHash);
            require(Deed(deed).owner() == msg.sender);
        }

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

    function getLabelHash(string name) internal returns(bytes32) {
        return sha3(name.toSlice().split(".".toSlice()).toString());
    }
}