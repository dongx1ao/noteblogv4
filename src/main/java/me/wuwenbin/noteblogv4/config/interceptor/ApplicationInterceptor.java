package me.wuwenbin.noteblogv4.config.interceptor;

import me.wuwenbin.noteblogv4.config.application.NBContext;
import me.wuwenbin.noteblogv4.model.entity.permission.NBSysUser;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 每个访问路径都需要做的一些操作
 * 譬如user的信息放入session
 * created by Wuwenbin on 2018/1/23 at 13:41
 *
 * @author wuwenbin
 */
public class ApplicationInterceptor extends HandlerInterceptorAdapter {

    private NBContext blogContext;

    public ApplicationInterceptor(NBContext blogContext) {
        this.blogContext = blogContext;
    }


    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
//        Cookie cookie = CookieUtils.getCookie(request, NoteBlogV4.Session.SESSION_ID_COOKIE);
//        if (cookie != null) {
//            String sessionId = cookie.getValue();
//            NBSession blogSession = blogContext.get(sessionId);
//            if (blogSession != null) {
//                blogSession.update();
//                if (modelAndView != null) {
//                    modelAndView.getModelMap().addAttribute("su", NBUtils.user2Map(blogSession.getSessionUser()));
//                }
//            }
//        }
        NBSysUser user = NBSysUser.builder().id(1L).defaultRoleId(1L).enable(true).build();
        blogContext.setSessionUser(request, response, user);
    }
}