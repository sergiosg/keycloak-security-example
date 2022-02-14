package io.wkrzywiec.keycloak.backend.movie;

import io.wkrzywiec.keycloak.backend.api.MoviesApi;
import io.wkrzywiec.keycloak.backend.api.dto.Movie;
import io.wkrzywiec.keycloak.backend.infra.security.annotation.AllowedRoles;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class MovieController implements MoviesApi {

    Map<Long, Movie> movies;

    public MovieController() {
        movies = Map.of(
                1L, new Movie().id(1).title("Star Wars: A New Hope").director( "George Lucas").year(1977),
                2L, new Movie().id(2).title("Star Wars: The Empire Strikes Back").director("Irvin Kershner").year(1980),
                3L, new Movie().id(3).title("Star Wars: Return of the Jedi").director("Richard Marquand").year(1983));
    }

    @Override
    @GetMapping("/movies")
    @AllowedRoles("ADMIN")
    public ResponseEntity<List<Movie>> listMovies(
            @Valid @RequestParam(value = "limit", required = false) Integer limit
    ) {
        SecurityContext sc = SecurityContextHolder.getContext();
        sc.getAuthentication().getAuthorities().forEach(b -> log.info(b.toString()));

        return new ResponseEntity<>(movies.values().stream().toList(), HttpStatus.OK);
    }

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/movies/{movieId}",
            produces = { "application/json" }
    )
    @AllowedRoles("VISITOR")
    public ResponseEntity<Movie> showMovieById( @PathVariable("movieId") String movieId ) {
        System.out.println("Movie id: " + movieId);
        return new ResponseEntity<>(movies.get(Long.valueOf(movieId)), HttpStatus.OK);
    }




}
