package com.etelligens.journalApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class JournalApplication {

	public static void main(String[] args) {

		SpringApplication.run(JournalApplication.class, args);

	}

	@Bean
	public PlatformTransactionManager add(MongoDatabaseFactory dbFactory){
		return new MongoTransactionManager(dbFactory);
	}
}


package com.etelligens.journalApplication.config;

import com.etelligens.journalApplication.service.UserDetailServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


@Configuration
@EnableWebSecurity
 public class SpringSecurity extends WebSecurityConfigurerAdapter {


    @Autowired
    private UserDetailServiceImpl userDetailsService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers(" /journal/**", "/user/**").authenticated()
                .anyRequest().permitAll()
                .and()
                .httpBasic();
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and().csrf().disable();
    }


    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Bean
    public  BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}



package com.etelligens.journalApplication.controller;

import com.etelligens.journalApplication.entity.JournalEntry;
import com.etelligens.journalApplication.entity.User;
import com.etelligens.journalApplication.service.JournalEntryService;
import com.etelligens.journalApplication.service.UserService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/journal")
public class JournalEntryController {

    @Autowired
    private JournalEntryService journalEntryService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<?> getAllJournalEntriesOfUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        List<JournalEntry> all = user.getJournalEntries();
        if(all !=null  && !all.isEmpty()){
            return new ResponseEntity<>(all, HttpStatus.OK);
        }
        return new  ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping
    public ResponseEntity<JournalEntry> createEntry(@RequestBody JournalEntry myEntry){
        try{
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
        journalEntryService.saveEntry(myEntry, username);
        return new ResponseEntity<>(myEntry, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }



    @GetMapping("id/{myId}")
    public ResponseEntity<?> getJournalEntryById(@PathVariable ObjectId myId){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        List<JournalEntry> collect = user.getJournalEntries().stream().filter(x -> x.getId().equals(myId)).collect(Collectors.toList());
        if (collect.isEmpty()){
            Optional<JournalEntry> journalEntry = journalEntryService.findById(myId);
            if(journalEntry.isPresent()){
                return new ResponseEntity<>(journalEntry.get(), HttpStatus.OK);
            }
        }
        return new  ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping ("id/{username}/{myId}")
    public ResponseEntity<?> deleterJournalEntryById(@PathVariable ObjectId myId , @PathVariable String username){
        journalEntryService.deleteById(myId , username);
         return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    @PutMapping("/id/{username}/{id}")
    public ResponseEntity<?> updateJournalById(
            @PathVariable ObjectId id,
            @RequestBody JournalEntry newEntry,
              @PathVariable  String username){

        JournalEntry old =   journalEntryService.findById(id).orElse(null);
     if(old!=null){
         old.setTitle(newEntry.getTitle() != null  && ! newEntry.getTitle().equals("") ?  newEntry.getTitle() : old.getTitle());
         old.setContent(newEntry.getContent() != null && !newEntry.getTitle().equals("") ? newEntry.getContent() : old.getContent());
         journalEntryService.saveEntry(old);
         return new ResponseEntity<>(old,HttpStatus.OK);
     }
     return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

}


package com.etelligens.journalApplication.controller;

import com.etelligens.journalApplication.entity.User;
import com.etelligens.journalApplication.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public")
public class PublicController {


    @Autowired
    private UserService userService;

    @GetMapping("/health-check")
    public String healthcheck(){
        return "Ok";
    }


    @PostMapping("/create-user")
    public boolean createUser(@RequestBody User user){
        userService.saveNewUser(user);
        return true;
    }
}


package com.etelligens.journalApplication.controller;


import com.etelligens.journalApplication.entity.User;
import com.etelligens.journalApplication.repository.UserRepository;
import com.etelligens.journalApplication.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;


    @PutMapping
    public ResponseEntity<?> UpdateUser(@RequestBody User user){
          Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
          String username = authentication.getName();
          User userInDB = userService.findByUsername(username);
          userInDB.setUsername(user.getUsername());
          userInDB.setPassword(user.getPassword());
          userService.saveNewUser(userInDB);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    public ResponseEntity<?> deleterUserById(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        userRepository.deleteByUsername(authentication.getName());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}


package com.etelligens.journalApplication.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "journal_entries")
@Data
@NoArgsConstructor
public class JournalEntry {

    @Id
    private ObjectId id;

    @NonNull
    private String title;

    private String content;

    public LocalDateTime date;


}


package com.etelligens.journalApplication.entity;

import lombok.Data;
import lombok.NonNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "users")
@Data
public class User {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    @NonNull
    private String username;
    @NonNull
    private String password;

    @DBRef
    private List<JournalEntry> journalEntries = new ArrayList<>();
    private List<String> roles;


}


package com.etelligens.journalApplication.repository;


import com.etelligens.journalApplication.entity.JournalEntry;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface JournalEntryRepository extends MongoRepository<JournalEntry, ObjectId>{

}


package com.etelligens.journalApplication.repository;


import com.etelligens.journalApplication.entity.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, ObjectId>{

    User findByUsername(String username);

   void  deleteByUsername(String username);
}
package com.etelligens.journalApplication.service;

import com.etelligens.journalApplication.entity.JournalEntry;
import com.etelligens.journalApplication.entity.User;
import com.etelligens.journalApplication.repository.JournalEntryRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

//@Slf4j
@Component
public class JournalEntryService {

    @Autowired
    private JournalEntryRepository  journalEntryRepository;

    @Autowired
    private UserService userService;

    @Transactional
    public void saveEntry(JournalEntry  journalEntry, String username){
       try{ User user = userService.findByUsername(username);
            journalEntry.setDate(LocalDateTime.now());
        JournalEntry saved = journalEntryRepository.save(journalEntry);
        user.getJournalEntries().add(saved);
        userService.saveUser(user);
       }catch (Exception e){
           System.out.println(e);
           throw new RuntimeException("An error occurred while saving the entry"+e);
       }
    }

    public void saveEntry(JournalEntry  journalEntry){
        journalEntryRepository.save(journalEntry);
    }

    public List<JournalEntry>getAll(){
        return journalEntryRepository.findAll();
    }

    public Optional<JournalEntry> findById(ObjectId id){
        return journalEntryRepository.findById(id);
    }

    public void deleteById(ObjectId id, String username){
        User user = userService.findByUsername(username);
        user.getJournalEntries().removeIf(x -> x.getId().equals(id));    // to remove on the point, if we do  not use this then also entry will be  removed but when user will save new entr
        userService.saveNewUser(user);
        journalEntryRepository.deleteById(id);
    }

//    public List<JournalEntry> findByUsername(String username){
//        return journalEntryRepository.findByUsername(username); ;
//    }
}


package com.etelligens.journalApplication.service;

import com.etelligens.journalApplication.entity.User;
import com.etelligens.journalApplication.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class UserDetailServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;



    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user!=null){
            return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .roles(user.getRoles().toArray(new String[0]))
                    .build();

        }
        throw new UsernameNotFoundException("User not found with username"+username);
    }
}


package com.etelligens.journalApplication.service;

import com.etelligens.journalApplication.entity.User;
import com.etelligens.journalApplication.repository.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

//@Slf4j
@Component
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void saveNewUser(User  user){
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(Arrays.asList("USER"));
            userRepository.save(user);
    }

    public void saveUser(User  user){
//       user.setPassword(passwordEncoder.encode(user.getPassword()));
//       user.setRoles(Arrays.asList("USER"));
        userRepository.save(user);
    }


    public List<User>getAll(){
        return userRepository.findAll();
    }

    public Optional<User> findById(ObjectId id){
        return userRepository.findById(id);
    }

    public void deleteById(ObjectId id){
        userRepository.deleteById(id);
    }

    public User findByUsername(String username){
        return userRepository.findByUsername(username);
    }



}

//PlatformTransactionManager
//MongoTransactionManager


