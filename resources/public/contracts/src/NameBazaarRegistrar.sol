pragma solidity ^0.5.17;

/**
 * @dev This contract is for testing purposes only. It poses as permanent
 * top level registrar governing the ownership of eth subdomains.
 */

import "@ensdomains/ethregistrar/contracts/BaseRegistrarImplementation.sol";

contract NameBazaarRegistrar is BaseRegistrarImplementation {
    /**
     * @dev Constructs a new Registrar, with the provided address as the owner of the root node.
     *
     * @param ens The address of the ENS
     * @param rootNode The hash of the rootnode.
     */
    constructor(ENS ens, bytes32 rootNode) BaseRegistrarImplementation(ens, rootNode) public {}

    /**
     * @dev Convenience function added by NameBazaar for instant registration
     *      used for development only
     *
     * @param _hash The sha3 hash of the label to register.
     */
    function register(bytes32 _hash) public payable {
        controllers[msg.sender] = true;
        emit ControllerAdded(msg.sender);
        _register(uint256(_hash), msg.sender, 365 days, true);
    }
}
