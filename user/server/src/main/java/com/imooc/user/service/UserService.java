package com.imooc.user.service;

import com.imooc.user.dataobject.UserInfo;

/**
 * Created by 廖师兄
 * 2018-03-04 21:44
 */
public interface UserService {

	/**
	 * 通过openid来查询用户信息
	 * @param openid
	 * @return
	 */
	UserInfo findByOpenid(String openid);
}
