pragma solidity ^0.5.17;

/**
 * @title UsedByFactories
 * @dev Provides modifiers to allow only offering factory contracts to execute method
 */

import "openzeppelin-solidity/contracts/ownership/Ownable.sol";

contract UsedByFactories is Ownable {

    mapping(address => bool) public isFactory;

    modifier onlyFactory() {
        require(isFactory[msg.sender]);
        _;
    }

    function setFactories(address[] memory factories, bool _isFactory)
        public
        onlyOwner
    {
        for(uint i = 0; i < factories.length; i++) {
            isFactory[factories[i]] = _isFactory;
        }
    }
}