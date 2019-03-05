package com.imooc.order.utils;

import com.imooc.order.VO.ResultVO;
import com.imooc.order.enums.ResultEnum;

/**
 * Created by 廖师兄
 * 2017-12-10 18:03
 */
public class ResultVOUtil {

    public static ResultVO success(Object object) {
        ResultVO resultVO = new ResultVO();
        resultVO.setCode(ResultEnum.SUCCESS.getCode());
        resultVO.setMsg(ResultEnum.SUCCESS.getMessage());
        resultVO.setData(object);
        return resultVO;
    }

    public static ResultVO seckillSuccess(String msg) {
        ResultVO resultVO = new ResultVO();
        resultVO.setCode(ResultEnum.SECKILL_WAIT.getCode());
        resultVO.setMsg(ResultEnum.SECKILL_WAIT.getMessage());
        resultVO.setData(msg);
        return resultVO;
    }

    public static ResultVO fail(String msg) {
        ResultVO resultVO = new ResultVO();
        resultVO.setCode(ResultEnum.FAIL.getCode());
        resultVO.setMsg(ResultEnum.FAIL.getMessage());
        resultVO.setData(msg);
        return resultVO;
    }
}
