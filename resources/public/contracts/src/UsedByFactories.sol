pragma solidity ^0.4.17;

/**
 * @title UsedByFactories
 * @dev Provides modifiers to allow only offering factory contracts to execute method
 */

import "Ownable.sol";

contract UsedByFactories is Ownable {

    mapping(address => bool) public isFactory;

    modifier onlyFactory() {
        require(isFactory[msg.sender]);
        _;
    }

    function setFactories(address[] factories, bool _isFactory)
        onlyOwner
    {
        for(uint i = 0; i < factories.length; i++) {
            isFactory[factories[i]] = _isFactory;
        }
    }
}